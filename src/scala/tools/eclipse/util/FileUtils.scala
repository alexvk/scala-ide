/*
 * Copyright 2005-2009 LAMP/EPFL
 */
// $Id$

package scala.tools.eclipse.util

import scala.collection.JavaConversions._

import org.eclipse.core.filebuffers.FileBuffers
import org.eclipse.core.resources.{ IFile, IMarker, IResource, IWorkspaceRunnable }
import org.eclipse.core.runtime.{ IProgressMonitor }
import org.eclipse.jdt.core.{ IJavaModelMarker, JavaCore }
import org.eclipse.jdt.core.compiler.IProblem
import org.eclipse.jdt.internal.core.builder.JavaBuilder
import org.eclipse.jface.text.{ ITextViewer, Position, TextPresentation }
import org.eclipse.jface.text.contentassist.ICompletionProposal
import org.eclipse.swt.widgets.Display
import org.eclipse.ui.{ IWorkbenchPage, PlatformUI }
import org.eclipse.ui.ide.IDE

import scala.tools.eclipse.ScalaPlugin

object FileUtils {
  import ScalaPlugin.plugin
  
  def length(file : IFile) = {
    val fs = FileBuffers.getFileStoreAtLocation(file.getLocation)
    if (fs != null)
      fs.fetchInfo.getLength.toInt
    else
      -1
  }
  
  def clearBuildErrors(file : IFile, monitor : IProgressMonitor) =
    file.getWorkspace.run(new IWorkspaceRunnable {
      def run(monitor : IProgressMonitor) = file.deleteMarkers(plugin.problemMarkerId, true, IResource.DEPTH_INFINITE)
    }, monitor)
  
  def clearTasks(file : IFile, monitor : IProgressMonitor) =
    file.getWorkspace.run(new IWorkspaceRunnable {
      def run(monitor : IProgressMonitor) = file.deleteMarkers(IJavaModelMarker.TASK_MARKER, true, IResource.DEPTH_INFINITE)
    }, monitor)
  
  def hasBuildErrors(file : IFile) : Boolean =
    file.findMarkers(plugin.problemMarkerId, true, IResource.DEPTH_INFINITE).exists(_.getAttribute(IMarker.SEVERITY) == IMarker.SEVERITY_ERROR)
  
  def buildError(file : IFile, severity : Int, msg : String, offset : Int, length : Int, line : Int, monitor : IProgressMonitor) =
    file.getWorkspace.run(new IWorkspaceRunnable {
      def run(monitor : IProgressMonitor) = {
        val mrk = file.createMarker(plugin.problemMarkerId)
        mrk.setAttribute(IMarker.SEVERITY, severity)
        
        val string = msg.map{
          case '\n' => ' '
          case '\r' => ' '
          case c => c
        }.mkString("","","")
        mrk.setAttribute(IMarker.MESSAGE , string)

        if (offset != -1) {
          mrk.setAttribute(IMarker.CHAR_START, offset)
          mrk.setAttribute(IMarker.CHAR_END, offset + length + 1)
          mrk.setAttribute(IMarker.LINE_NUMBER, line)
        }
      }
    }, monitor)

  def task(file : IFile, tag : String, msg : String, priority : String, offset : Int, length : Int, line : Int, monitor : IProgressMonitor) =
    file.getWorkspace.run(new IWorkspaceRunnable {
      def run(monitor : IProgressMonitor) = {
        val mrk = file.createMarker(IJavaModelMarker.TASK_MARKER)
        val values = new Array[AnyRef](taskMarkerAttributeNames.length)

        val prioNum = priority match {
          case JavaCore.COMPILER_TASK_PRIORITY_HIGH => IMarker.PRIORITY_HIGH
          case JavaCore.COMPILER_TASK_PRIORITY_LOW => IMarker.PRIORITY_LOW
          case _ => IMarker.PRIORITY_NORMAL
        }
        
        values(0) = tag+" "+msg
        values(1) = Integer.valueOf(prioNum)
        values(2) = Integer.valueOf(IProblem.Task)
        values(3) = Integer.valueOf(offset)
        values(4) = Integer.valueOf(offset + length + 1)
        values(5) = Integer.valueOf(line)
        values(6) = java.lang.Boolean.valueOf(false)
        values(7) = JavaBuilder.SOURCE_ID
        mrk.setAttributes(taskMarkerAttributeNames, values);
      }
    }, monitor)

  private val taskMarkerAttributeNames = Array(
    IMarker.MESSAGE,
    IMarker.PRIORITY,
    IJavaModelMarker.ID,
    IMarker.CHAR_START,
    IMarker.CHAR_END,
    IMarker.LINE_NUMBER,
    IMarker.USER_EDITABLE,
    IMarker.SOURCE_ID
  )
}